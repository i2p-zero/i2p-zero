How to run a mipseed:
1. Download the Monero CLI version 0.14.1.2 (it is important to use this version or later). Download links as usual are here: https://web.getmonero.org/downloads/ Important: do NOT use the GUI version, because that release is currently only version 0.14.1.0
2. Download and unzip the latest (non-GUI) version of I2P-zero here: https://github.com/i2p-zero/i2p-zero/releases
3. Run I2P-zero by entering the i2p-zero unzipped directory and  typing: `router/bin/launch.sh`
4. Create a socks tunnel for outgoing I2P connections by typing: `router/bin/tunnel-control.sh socks.create 8060`
5. Create a server tunnel for incoming I2P connections by typing: `router/bin/tunnel-control.sh server.create.vanity 127.0.0.1 8061 none xyz` replacing `xyz` with a 3 alphanumeric character vanity prefix for your public b32.i2p address. This command will take a few minutes to complete, depending on how fast your CPU is. If you do not want a vanity prefix, use the command: `router/bin/tunnel-control.sh server.create 127.0.0.1 8061`
6. The command above will result in an I2P address being printed to the command line, which will end with `.b32.i2p`. This is your new I2P address.
7. You now need to keep a backup of your private keys to your I2P address on another computer for safekeeping. Do this by taking a backup of the `~/.i2p-zero/config/tunnels.json` file
8. Run monerod by typing the following, replacing `XXXXXXXXXXXXXXXXXXXXXXXXXXXXX.b32.i2p` with your own I2P address that was printed from step 5: `monerod --proxy i2p,127.0.0.1:8060 --add-peer dsc7fyzzultm7y6pmx2avu6tze3usc7d27nkbzs5qwuujplxcmzq.b32.i2p --anonymous-inbound XXXXXXXXXXXXXXXXXXXXXXXXXXXXX.b32.i2p,127.0.0.1:8061 --prune-blockchain --detach`
9. (optional) Use software such as wondershaper for Linux to limit bandwidth usage. See this: https://www.ostechnix.com/how-to-limit-network-bandwidth-in-linux-using-wondershaper/
