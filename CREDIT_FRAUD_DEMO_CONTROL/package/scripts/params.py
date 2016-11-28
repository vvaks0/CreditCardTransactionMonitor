
#!/usr/bin/env python
from resource_management import *

# server configurations
config = Script.get_config()

install_dir = config['configurations']['control-config']['democontrol.install_dir']
download_url = config['configurations']['control-config']['democontrol.download_url']
nifi_host_ip = config['configurations']['control-config']['democontrol.nifi_host_ip']
