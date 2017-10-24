
#!/usr/bin/env python
from resource_management import *

# server configurations
config = Script.get_config()

download_url = config['configurations']['control-config']['democontrol.download_url']
install_dir = config['configurations']['control-config']['democontrol.install.dir']
simulator_download_url = config['configurations']['control-config']['democontrol.simulator.git.url']
device_manager_download_url = config['configurations']['control-config']['democontrol.device.manager.git.url']
sam_extentions_download_url = config['configurations']['control-config']['democontrol.sam.extentions.git.url']
google_api_key = config['configurations']['control-config']['democontrol.google.api.key']

#nifi_host_ip = config['configurations']['control-config']['democontrol.nifi_host_ip']
nifi_host = str(config['clusterHostInfo']['nifi_master_hosts'][0])
nifi_port = '9090'