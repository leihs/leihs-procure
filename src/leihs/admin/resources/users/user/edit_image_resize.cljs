(ns leihs.admin.resources.users.user.edit-image-resize
  (:require
   ["@jimp/custom"]
   ["jimp" :as Jimp]))

(defn resize-to-b64
  [data max-dimension & {:keys [error-handler success-handler]
                         :or {error-handler #()
                              success-handler #()}}]
  (.read Jimp data
         (fn [err ^js img]
           (if err
             (error-handler err)
             (do (doto img
                   (.resize max-dimension max-dimension)
                   (.quality 80))
                 (.getBase64 img "image/jpeg"
                             (fn [err b64]
                               (if err
                                 (error-handler err)
                                 (success-handler b64)))))))))
