(ns leihs.procurement.scratch
 (:refer-clojure :exclude [str keyword])
 (:require
  [leihs.core.core :refer [keyword str presence]]
  [taoensso.timbre :refer [debug info warn error spy]])

 (:import
   [java.util EnumSet ]
   [java.nio.file FileVisitOption]
   ))

; (EnumSet/of FileVisitOption/FOLLOW_LINKS)



