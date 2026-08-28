package com.emme.ai.contracts.extraction;

/** Provider-neutral boundary for validated nail-design extraction. */
public interface NailDesignExtractionPort {

  NailDesignFeatures extract(String imageReference);
}
