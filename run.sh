#!/bin/bash
cd build/libs &&\
java -jar oanda-2.0.jar –Xmx1024m –Xms1024m -XX:OnOutOfMemoryError="kill %p" -XX:AutoBoxCacheMax=512

