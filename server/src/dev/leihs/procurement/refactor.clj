(ns leihs.procurement.refactor)

(defn thread-first-h
  ([f-app] (thread-first-h f-app []))
  ([[f first-arg & rst] result]
   (let [f-without-first (if (nil? rst) f `(~f ~@rst))]
     (if (seq? first-arg)
       (thread-first-h first-arg (cons f-without-first result))
       (cons first-arg (cons f-without-first result))))))

(defmacro thread-first-macro [f-app] (cons '-> (thread-first-h f-app)))

(defmacro thread-first [form] `(macroexpand-1 '(thread-first-macro ~form)))

(defn thread-last-h
  ([f-app] (thread-last-h f-app []))
  ([[f & rst] result]
   (let [[last-arg & rev-rst] (reverse rst)
         f-without-last (if (nil? rev-rst) f `(~f ~@(reverse rev-rst)))]
     (if (seq? last-arg)
       (thread-last-h last-arg (cons f-without-last result))
       (cons last-arg (cons f-without-last result))))))

(defmacro thread-last-macro [f-app] (cons '->> (thread-last-h f-app)))

(defmacro thread-last [form] `(macroexpand-1 '(thread-last-macro ~form)))
