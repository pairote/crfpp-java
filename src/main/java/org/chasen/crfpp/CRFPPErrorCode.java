package org.chasen.crfpp;

public enum CRFPPErrorCode {
  
  UNKNOWN(0), FAILED_TO_LOAD_NATIVE_LIBRARY(1);
  
  public final int id;
  
  private CRFPPErrorCode(int id) {
    this.id = id;
  }
  
  public static CRFPPErrorCode getErrorCode(int id) {
    for (CRFPPErrorCode code : CRFPPErrorCode.values()) {
      if (code.id == id)
        return code;
    }
    return UNKNOWN;
  }
  
  public static String getErrorMessage(int id) {
    return getErrorCode(id).name();
  }
  
}
