import { ConvertToHexAARRGGBB } from '../convert-color';

type Config = Record<string, any>;

const replacePrefix = (data: Config, index = 1): Config => {
  return Object.entries(data).reduce((result: Config, [key, value]) => {
    const parts = key.split('_');
    const prefixIndex = Math.min(index, parts.length - 1);
    result[parts.slice(prefixIndex).join('_')] = value;
    return result;
  }, {});
};

const pickConfig = (config: Config, keys: string[]): Config => {
  return keys.reduce((picked: Config, key: string) => {
    if (key in config) {
      picked[key] = config[key];
      delete config[key];
    }
    return picked;
  }, {});
};

const parseUdpPorts = (udpPorts: string | undefined): number[] | null => {
  if (!udpPorts) return null;
  return udpPorts.split(',').map((value) => parseInt(value, 10));
};

const parseAuthConfig = (config: Config): Config => {
  const auth = replacePrefix(pickConfig(config, ['auth_password', 'auth_username', 'auth_v2ray_uuid']));
  auth.username = auth.username || null;
  auth.password = auth.password || null;
  return auth;
};

const parseCommonConfig = (config: Config): void => {
  config.cdns = null;
  config.proxy = replacePrefix(pickConfig(config, ['proxy_host', 'proxy_port']));
  config.server = replacePrefix(pickConfig(config, ['server_host', 'server_port']));
  config.dns_server = replacePrefix(pickConfig(config, ['dns_server_dns1', 'dns_server_dns2']), 2);
  config.config_payload = replacePrefix(pickConfig(config, ['config_payload_payload', 'config_payload_sni']), 2);
  config.udp_ports = parseUdpPorts(config.udp_ports);
};

export function AppConfigParser(config: Config): Config {
  if (config.category) {
    config.category.color = ConvertToHexAARRGGBB(config.category.color);
  }

  config.auth = parseAuthConfig(config);
  parseCommonConfig(config);

  return config;
}

export function AppConfigParserApi(config: Config): Config {
  if (config.category) {
    delete config.category;
  }
  if (config.created_at) {
    delete config.created_at;
  }
  if (config.updated_at) {
    delete config.updated_at;
  }

  config.auth = parseAuthConfig(config);
  parseCommonConfig(config);

  return config;
}
