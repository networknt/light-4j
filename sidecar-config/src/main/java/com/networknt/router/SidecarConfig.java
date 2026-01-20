package com.networknt.router;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema; // REQUIRED IMPORT
import com.networknt.config.schema.OutputFormat; // REQUIRED IMPORT
import com.networknt.config.schema.StringField; // REQUIRED IMPORT
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

    private Map<String, Object> mappedConfig;
    private final Config config;

    // --- Annotated Field ---
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

    // --- Constructor and Loading Logic ---

    private SidecarConfig() {
        this(CONFIG_NAME);
    }
    private SidecarConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static SidecarConfig load(String configName) {
        return new SidecarConfig(configName);
    }

    public static SidecarConfig load() {
        return new SidecarConfig();
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
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
