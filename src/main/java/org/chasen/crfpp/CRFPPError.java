package org.chasen.crfpp;

public class CRFPPError extends Error {
  
  private static final long serialVersionUID = 1L;
  
  public final CRFPPErrorCode errorCode;
  
  public CRFPPError(CRFPPErrorCode code) {
    super();
    this.errorCode = code;
  }
  
  public CRFPPError(CRFPPErrorCode code, Error e) {
    super(e);
    this.errorCode = code;
  }
  
  public CRFPPError(CRFPPErrorCode code, String message) {
    super(message);
    this.errorCode = code;
  }
  
  @Override
  public String getMessage() {
    return String.format("[%s] %s", errorCode.name(), super.getMessage());
  }
  
}
