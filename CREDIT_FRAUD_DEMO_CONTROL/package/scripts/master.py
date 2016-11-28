import sys, os, pwd, signal, time, shutil
from subprocess import *
from resource_management import *

class DemoControl(Script):
  def install(self, env):
    self.configure(env)
    import params
  
    if not os.path.exists(params.install_dir):  
        os.makedirs(params.install_dir)
    os.chdir(params.install_dir)
    if not os.path.exists(params.install_dir+'/CreditCardTransactionMonitor'):
        Execute('git clone ' + params.download_url)
    os.chdir(params.install_dir+'/CreditCardTransactionMonitor/CreditCardTransactionSimulator')
    Execute('mvn clean package')
    os.chdir(params.install_dir+'/CreditCardTransactionMonitor/CreditCardTransactionSimulator/target')
    shutil.copy('CreditCardTransactionSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar', params.install_dir)

  def start(self, env):
    self.configure(env)
    import params
    Execute('echo Start Simulation')
    Execute('nohup java -jar '+params.install_dir+'/CreditCardTransactionSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar Customer 1000 Simulation '+params.nifi_host_ip+' > '+params.install_dir+'/CreditCardTransactionSim.log 2>&1 & echo $! > /var/run/CreditCardTransactionSim.pid')
    
  def stop(self, env):
    self.configure(env)
    import params
    Execute('echo Stop Simulation')
    Execute (format('kill -9 `cat /var/run/CreditCardTransactionSim.pid` >/dev/null 2>&1')) 
    Execute ('rm -f /var/run/CreditCardTransactionSim.pid')
    
  def status(self, env):
    import params
    env.set_params(params)
    check_process_status('/var/run/CreditCardTransactionSim.pid')
    
  def configure(self, env):
    import params
    env.set_params(params)

if __name__ == "__main__":
  DemoControl().execute()
