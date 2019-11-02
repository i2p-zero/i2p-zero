# How to run a mipseed:
1. Download the Monero CLI version 0.14.1.2 (it is important to use this version or later). Download links as usual are here: https://web.getmonero.org/downloads/ Important: do NOT use the GUI version, because that release is currently only version 0.14.1.0
2. Download and unzip the latest (non-GUI) version of I2P-zero here: https://github.com/i2p-zero/i2p-zero/releases
3. Run I2P-zero by entering the i2p-zero unzipped directory and  typing: `router/bin/launch.sh`
4. (optional) Find out your randomly assigned I2P port by typing: `router/bin/tunnel-control.sh router.externalPort`. For privacy reasons, do not disclose this port number to other people. Tell your firewall to forward traffic through to this port so that your I2P node is publicly reachable. If you have no ability to allow incoming connections, everything will still work, but your I2P node will not be helping the I2P network as much as it could.
5. Create a socks tunnel for outgoing I2P connections by typing: `router/bin/tunnel-control.sh socks.create 8060`
6. Create a server tunnel for incoming I2P connections by typing: `router/bin/tunnel-control.sh server.create.vanity 127.0.0.1 8061 none xyz` replacing `xyz` with a 3 alphanumeric character vanity prefix for your public b32.i2p address. This command will take a few minutes to complete, depending on how fast your CPU is. If you do not want a vanity prefix, use the command: `router/bin/tunnel-control.sh server.create 127.0.0.1 8061`
7. The command above will result in an I2P address being printed to the command line, which will end with `.b32.i2p`. This is your new I2P address.
8. You now need to keep a backup of your private keys to your I2P address on another computer for safekeeping. Do this by taking a backup of the `~/.i2p-zero/config/tunnels.json` file
9. Run monerod by typing the following, replacing `XXXXXXXXXXXXXXXXXXXXXXXXXXXXX.b32.i2p` with your own I2P address that was printed from step 5: `monerod --proxy i2p,127.0.0.1:8060 --add-peer dsc7fyzzultm7y6pmx2avu6tze3usc7d27nkbzs5qwuujplxcmzq.b32.i2p --anonymous-inbound XXXXXXXXXXXXXXXXXXXXXXXXXXXXX.b32.i2p,127.0.0.1:8061 --prune-blockchain --detach`. Note than in future Monero versions, you will have to replace "--proxy" with "--tx-proxy"
10. (optional) Use software such as wondershaper for Linux to limit bandwidth usage. See this: https://www.ostechnix.com/how-to-limit-network-bandwidth-in-linux-using-wondershaper/

That's it! Do not replace the dsc****.b32.i2p address with yours, only replace the XXXXXXX.b32.i2p one. You are now running a mipseed, which will itself be seeded by dsc_'s mipseed.

If you are running Linux, it would be useful to set this all up to run automatically if the machine is ever rebooted. You can do this by creating systemd service files, as documented by dsc_ here: https://gist.github.com/xmrdsc/2f2f0ce7a2d099f22e55ba9e4fe1bfba That gist file also contains more detailed information about how this setup works.

## What this all about:
Monero now has I2P support. Soon, the GUI will make it easy for anyone to enable I2P.

When people run monerod with I2P enabled, they will need to connect to a "mipseed" to discover all of the I2P addresses where other people are running I2P-enabled monero nodes.

A "mipseed", which is short for "monero I2P seed", is simply a computer running a monerod instance that is accessible via I2P.

Your mipseed will only be there to inform others of I2P addresses it knows about, and to share new transaction announcements via I2P.

Because you will be running your mipseed behind I2P, disclosing your I2P address will not disclose to anyone your identity, your IP address or even your country.
