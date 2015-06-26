# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "hashicorp/precise64"
  config.vm.provision :shell, path: "dev-setup/bootstrap.sh"
  #config.vm.provision "ansible" do |ansible|
  #  ansible.playbook = "dev-setup/playbook.yml"
  #end
  config.vm.network :forwarded_port, host: 4567, guest: 9443
  config.vm.network :forwarded_port, host: 3567, guest: 9000
  config.vm.network :forwarded_port, host: 9999, guest: 9999
end
