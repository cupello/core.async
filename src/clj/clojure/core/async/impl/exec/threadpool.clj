;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns clojure.core.async.impl.exec.threadpool
  (:require [clojure.core.async.impl.protocols :as impl])
  (:import [java.util.concurrent Executors ThreadFactory ExecutorService]
           [clojure.jsr166y ForkJoinPool ForkJoinTask]))

(set! *warn-on-reflection* true)

(defn counted-thread-factory
  "Create a ThreadFactory that maintains a counter for naming Threads.
     name-format specifies thread names - use %d to include counter
     daemon is a flag for whether threads are daemons or not"
  [name-format daemon]
  (let [counter (atom 0)]
    (reify
      ThreadFactory
      (newThread [this runnable]
        (doto (Thread. runnable)
          (.setName (format name-format (swap! counter inc)))
          (.setDaemon daemon))))))

(def processors
  "Number of processors reported by Java"
  (.availableProcessors (Runtime/getRuntime)))

(defonce the-executor
  (Executors/newFixedThreadPool
   (+ 2 processors)
   (counted-thread-factory "async-dispatch-%d" true)))

(defn thread-pool-executor
  ([] (thread-pool-executor the-executor))
  ([^ExecutorService executor-svc]
     (reify impl/Executor
       (impl/exec [this r]
         (.submit executor-svc ^Runnable r)
         nil))))


