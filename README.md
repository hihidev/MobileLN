Overview
--------

Decentralize the world!

Mobile LN = Bitcoin node + Lightning node app on Android.



Features
--------

- Bitcoin node (bitcoind)
- Lightning node (c-lightning)
- Simple Bitcoin + Lightning wallet UI
- Fast init sync (Download utxo snapshots)
- Dev console (direct commands to lightning-cli)



External libraries/services used
--------------------------------

clightning_ndk_build (https://github.com/hihidev/clightning_ndk_build)
bitcoin_ndk_build (https://github.com/hihidev/bitcoin_ndk_build)
ZXing Android Embedded (https://github.com/journeyapps/zxing-android-embedded)
ZXing (https://github.com/zxing/zxing)
bitcoin-rpc-client (https://github.com/Polve/bitcoin-rpc-client)
Apache Common
Google Material Design
utxo snapshots from btcpayserver (https://github.com/btcpayserver/btcpayserver-docker/tree/master/contrib/FastSync)



License
-------

Apache License 2.0



Contribute
----------

Let's decentralize the work and contribution!

Major work / improvement before it is "usable":
(I'm not a superman and cannot do all the work.)

- Better UX, clean drawables
- CI and testing code
- Update bitcoind and c-lightning repo, and update the api change
- Fix all minor / edge cases / null pointers
- Strings!

