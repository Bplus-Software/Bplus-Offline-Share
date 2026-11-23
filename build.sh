#!/bin/bash
set -e
export JAVA_HOME=~/jdk17
SDK=~/android-sdk
BT=$SDK/build-tools/34.0.0
AJAR=$SDK/platforms/android-34/android.jar
APP=~/bplus-drop/app
OUT=~/bplus-drop/build
rm -rf $OUT && mkdir -p $OUT/res $OUT/gen $OUT/classes $OUT/dex
# 1. compile resources
$BT/aapt2 compile --dir $APP/res -o $OUT/res/res.zip
# 2. link
$BT/aapt2 link -o $OUT/app-unaligned.apk -I $AJAR --manifest $APP/AndroidManifest.xml \
  --java $OUT/gen -A $APP/assets $OUT/res/res.zip
# 3. javac
find $APP/src $OUT/gen -name '*.java' > $OUT/sources.txt
$JAVA_HOME/bin/java -jar ~/bplus-drop/ecj.jar -8 -encoding UTF-8 -bootclasspath $AJAR:$HOME/bplus-drop/lambda-stub.jar -cp $APP/libs/zxing-core.jar -d $OUT/classes @$OUT/sources.txt
# 4. dex (app classes + zxing)
cd $OUT/classes && find . -name '*.class' | xargs $JAVA_HOME/bin/jar cf $OUT/app-classes.jar && cd -
$BT/d8 --min-api 26 --lib $AJAR --output $OUT/dex $OUT/app-classes.jar $APP/libs/zxing-core.jar
# 5. add dex to apk
cp $OUT/app-unaligned.apk $OUT/app-dex.apk
cd $OUT/dex && zip -q -X $OUT/app-dex.apk classes.dex && cd -
# 6. align + sign
$BT/zipalign -f 4 $OUT/app-dex.apk $OUT/app-aligned.apk
$BT/apksigner sign --ks ~/bplus-drop/bplus-share.keystore --ks-pass pass:bplusshare123 --out ~/bplus-drop/BplusShare.apk $OUT/app-aligned.apk
$BT/apksigner verify --print-certs ~/bplus-drop/BplusShare.apk | head -3
ls -la ~/bplus-drop/BplusShare.apk
