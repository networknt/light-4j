package com.networknt.router;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema; // REQUIRED IMPORT
import com.networknt.config.schema.OutputFormat; // REQUIRED IMPORT
import com.networknt.config.schema.StringField; // REQUIRED IMPORT
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Config class for gateway.
 *
 */
@ConfigSchema(
        configKey = "sidecar",
        configName = "sidecar",
        configDescription = "Light http sidecar configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class SidecarConfig {
    private static final Logger logger = LoggerFactory.getLogger(SidecarConfig.class);

    public static final String CONFIG_NAME = "sidecar";
    private static final String EGRESS_INGRESS_INDICATOR = "egressIngressIndicator";

    private static volatile SidecarConfig instance;
    private final Config config;
    private Map<String, Object> mappedConfig;

    @StringField(
            configFieldName = EGRESS_INGRESS_INDICATOR,
            externalizedKeyName = EGRESS_INGRESS_INDICATOR,
            description = "Indicator used to determine the condition for router traffic in http-sidecar or light-gateway. Default is 'header'.\n" +
                    "If the egressIngressIndicator set as header, sidecar will router request based on if the request header has service id/service url or not. This will be default setting\n" +
                    "If the egressIngressIndicator set protocol, sidecar will router request based on protocol. normally the traffic inside pod will http (from api container to sidecar container), and sidecar will treat http traffic as egress router\n" +
                    "If the egressIngressIndicator set as other values, currently sidecar will skip router handler and leave the request traffic to proxy\n",
            defaultValue = "header"
    )
    String egressIngressIndicator;

    private SidecarConfig() {
        this(CONFIG_NAME);
    }

    private SidecarConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        if (mappedConfig != null) {
            setConfigData();
        }
    }

    public static SidecarConfig load() {
        return load(CONFIG_NAME);
    }

    public static SidecarConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            if (instance != null && instance.getMappedConfig() == Config.getInstance().getJsonMapConfig(configName)) {
                return instance;
            }
            synchronized (SidecarConfig.class) {
                if (instance != null && instance.getMappedConfig() == Config.getInstance().getJsonMapConfig(configName)) {
                    return instance;
                }
                instance = new SidecarConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, SidecarConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
                return instance;
            }
        }
        return new SidecarConfig(configName);
    }

    public void reload() {
        instance = new SidecarConfig(CONFIG_NAME);
        ModuleRegistry.registerModule(CONFIG_NAME, SidecarConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    public String getEgressIngressIndicator() {
        return egressIngressIndicator;
    }

    public void setEgressIngressIndicator(String egressIngressIndicator) {
        this.egressIngressIndicator = egressIngressIndicator;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(EGRESS_INGRESS_INDICATOR);
            if(object != null) egressIngressIndicator = (String)object;
        }
    }
}
