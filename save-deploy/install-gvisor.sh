#!/usr/bin/env sh

# Script from https://gvisor.dev/docs/user_guide/install/#install-latest

cat > gvisor-install.sh << 'EOF'
ARCH=$(uname -m)
URL=https://storage.googleapis.com/gvisor/releases/release/latest/${ARCH}
curl ${URL}/runsc -o runsc
curl ${URL}/runsc.sha512 -o runsc.sha512
curl ${URL}/containerd-shim-runsc-v1 -o containerd-shim-runsc-v1
curl ${URL}/containerd-shim-runsc-v1.sha512 -o containerd-shim-runsc-v1.sha512
sha512sum -c runsc.sha512 -c containerd-shim-runsc-v1.sha512
rm -f *.sha512
chmod a+rx runsc containerd-shim-runsc-v1
mv runsc containerd-shim-runsc-v1 /usr/local/bin
EOF
cp gvisor-install.sh /k8s-node

# Run the following steps on the host system
/usr/bin/nsenter -m/proc/1/ns/mnt -- sh /tmp/gvisor/gvisor-install.sh
/usr/bin/nsenter -m/proc/1/ns/mnt -- /usr/local/bin/runsc install
# Add gvisor to containerd config (https://gvisor.dev/docs/user_guide/containerd/configuration/)
/usr/bin/nsenter -m/proc/1/ns/mnt -- cat <<EOF | tee -a /etc/containerd/config.toml
version = 2
[plugins."io.containerd.grpc.v1.cri".containerd.runtimes.runsc]
  runtime_type = "io.containerd.runsc.v1"
[plugins."io.containerd.grpc.v1.cri".containerd.runtimes.runsc.options]
  TypeUrl = "io.containerd.runsc.v1.options"
  ConfigPath = "/etc/containerd/runsc.toml"
EOF
/usr/bin/nsenter -m/proc/1/ns/mnt -- cat <<EOF | sudo tee /etc/containerd/runsc.toml
log_path = "/var/log/runsc/%ID%/shim.log"
log_level = "debug"
[runsc_config]
  network = "host"
  debug = "true"
  debug-log = "/var/log/runsc/%ID%/gvisor.%COMMAND%.log"
EOF
/usr/bin/nsenter -m/proc/1/ns/mnt -- systemctl restart containerd

echo "Finished"
sleep infinity