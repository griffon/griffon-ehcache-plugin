/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.ehcache

import net.sf.ehcache.Cache
import net.sf.ehcache.CacheManager
import net.sf.ehcache.config.CacheConfiguration
import net.sf.ehcache.config.Configuration

import griffon.core.GriffonApplication
import griffon.util.ConfigUtils

/**
 * @author Andres Almiray
 */
@Singleton
final class EhcacheConnector {
    private bootstrap

    private static final String CLASSPATH_PREFIX = 'classpath://'
    private static final String DEFAULT = 'default'

    ConfigObject createConfig(GriffonApplication app) {
        if (!app.config.pluginConfig.ehcache) {
            app.config.pluginConfig.ehcache = ConfigUtils.loadConfigWithI18n('EhcacheConfig')
        }
        app.config.pluginConfig.ehcache
    }

    private ConfigObject narrowConfig(ConfigObject config, String cacheManagerName) {
        if (config.containsKey('cacheManager') && cacheManagerName == DEFAULT) {
            return config.cacheManager
        } else if (config.containsKey('cacheManagers')) {
            return config.cacheManagers[cacheManagerName]
        }
        return config
    }

    CacheManager connect(GriffonApplication app, ConfigObject config, String cacheManagerName = DEFAULT) {
        if (CacheManagerHolder.instance.isCacheManagerConnected(cacheManagerName)) {
            return CacheManagerHolder.instance.getCacheManager(cacheManagerName)
        }

        config = narrowConfig(config, cacheManagerName)
        app.event('EhcacheConnectStart', [config, cacheManagerName])
        CacheManager cacheManager = startEhcache(app, config)
        CacheManagerHolder.instance.setCacheManager(cacheManagerName, cacheManager)
        bootstrap = app.class.classLoader.loadClass('BootstrapEhcache').newInstance()
        bootstrap.metaClass.app = app
        resolveEhcacheProvider(app).withEhcache(cacheManagerName) { cmn, cm -> bootstrap.init(cmn, cm) }
        app.event('EhcacheConnectEnd', [cacheManagerName, cacheManager])
        cacheManager
    }

    void disconnect(GriffonApplication app, ConfigObject config, String cacheManagerName = DEFAULT) {
        if (CacheManagerHolder.instance.isCacheManagerConnected(cacheManagerName)) {
            config = narrowConfig(config, cacheManagerName)
            CacheManager cacheManager = CacheManagerHolder.instance.getCacheManager(cacheManagerName)
            app.event('EhcacheDisconnectStart', [config, cacheManagerName, cacheManager])
            resolveEhcacheProvider(app).withEhcache(cacheManagerName) { cmn, cm -> bootstrap.destroy(cmn, cm) }
            stopEhcache(config, cacheManager)
            app.event('EhcacheDisconnectEnd', [config, cacheManagerName])
            CacheManagerHolder.instance.disconnectCacheManager(cacheManagerName)
        }
    }

    EhcacheProvider resolveEhcacheProvider(GriffonApplication app) {
        def ehcacheProvider = app.config.ehcacheProvider
        if (ehcacheProvider instanceof Class) {
            ehcacheProvider = ehcacheProvider.newInstance()
            app.config.ehcacheProvider = ehcacheProvider
        } else if (!ehcacheProvider) {
            ehcacheProvider = DefaultEhcacheProvider.instance
            app.config.ehcacheProvider = ehcacheProvider
        }
        ehcacheProvider
    }

    private CacheManager startEhcache(GriffonApplication app, ConfigObject config) {
        if (config.url) {
            if (config.url instanceof URL) return new CacheManager(config.url)
            String url = config.url.toString()
            if (url.startsWith(CLASSPATH_PREFIX)) return new CacheManager(app.getResourceAsURL(url.substring(CLASSPATH_PREFIX.length())))
            return new CacheManager(url.toURL())
        }
        Configuration cacheManagerConfig = new Configuration()
        List caches = []
        config.each { key, value ->
            if (key == 'caches') {
                value.each { cacheName, cacheConfigProps ->
                    CacheConfiguration cacheConfiguration = new CacheConfiguration()
                    cacheConfigProps.each { k, v ->
                        cacheConfiguration[k] = v
                    }
                    cacheConfiguration.name = cacheName
                    caches << new Cache(cacheConfiguration)
                }
            } else {
                try {
                    cacheManagerConfig[key] = value
                } catch (MissingPropertyException x) {
                    // ignore ?
                }
            }
        }
        CacheManager cacheManager = new CacheManager(cacheManagerConfig)
        caches.each { cache -> cacheManager.addCache(cache) }
        cacheManager
    }

    private void stopEhcache(ConfigObject config, CacheManager cacheManager) {
        cacheManager.shutdown()
    }
}