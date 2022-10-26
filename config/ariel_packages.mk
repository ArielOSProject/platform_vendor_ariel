PRODUCT_PACKAGES += \
  AuroraStore \
  DejaVu \
  GmsCore \
  GsfProxy \
  FakeStore \
  IchnaeaNlpBackend \
  NominatimGeocoderBackend \
  ArielGuardian \
  NewPipe
  #NetworkLocation -> this is now part of gms core from microg
  #com.google.android.maps.jar \


# ArielGuardian
#  arielfw - excluded until we create a system service with daemon connector
#            check: https://github.com/ArielOSProject/platform_vendor_ariel/blob/master_ditch_shared_system_id/platform/ariel/lib/main/java/com/ariel/platform/internal/firewall/ArielFirewallService.java
  