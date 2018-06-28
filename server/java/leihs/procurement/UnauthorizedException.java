package leihs.procurement;

import clojure.lang.ExceptionInfo;
import clojure.lang.IPersistentMap;

public class UnauthorizedException extends ExceptionInfo {
  public UnauthorizedException(String s, IPersistentMap data) {
    super(s, data);
  }
}
