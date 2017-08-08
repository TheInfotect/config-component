(ns datamos.config.core
  (:gen-class)
  (:require [mount.core :as mnt :refer [defstate]]
            [datamos
             [core :as dc]
             [communication :as dcom]
             [base :as base]
             [rdf-function :as rdf-fn]
             [messaging :as dm]]
            [clojure.core.async :as async]))

(defonce ^:private config (atom {}))

(defonce ^:private the-registry (atom {}))

(defonce ^:private remote-components (atom {}))

(def registry-predicates-set
  #{:rdf/type :rdfs/label :dms-def/function :dms-def/provides})

(defstate ^{:on-reoload :noop} config-settings :start @config)

(defstate ^{:on-reload :noop} config-connection
          :start (dm/rmq-connection)
          :stop (dm/close config-connection))

(defstate ^{:on-reload :noop} config-queue
          :start (dm/set-queue config-connection config-settings)
          :stop (dm/remove-queue config-connection config-queue))

(defstate config-local-channel
          :start (dcom/channel))

(defstate config-listener
          :start (dcom/listen config-connection config-local-channel config-queue)
          :stop (dcom/close-listen config-listener))

(defstate config-responder
          :start (dcom/response config-local-channel base/component)
          :stop (async/close! config-responder))

(defn registration
  [_ _ message]
  (let [msg-header (:datamos/logistic message)
        sender-uri (apply first (filter (fn [x] (= :dms-def/sender (rdf-fn/value-from-nested-map (conj {} x)))) (rdf-fn/predicate-filter msg-header #{:dms-def/transmit})))]
    (swap!
      the-registry
      conj
      (rdf-fn/predicate-filter
        (rdf-fn/message-content message)
        registry-predicates-set))
    #_(println "@registration - Yep, message arrived here as well!\n" message)
    (dcom/speak dcom/speak-connection dm/exchange base/component sender-uri :dms-def/component :datamos/registry @the-registry)))

(defn local-register
  []
  @remote-components)

(defn register
  [_ _ message]
  (let [rdf-content (rdf-fn/message-content message)
        r (local-register)
        values (rdf-fn/values-by-predicate :dms-def/function
                                           rdf-content
                                           r)]
    #_(println "@register - endpoint of the line:\n" message)
    (when (apply = values)
      (do
        #_(println "doing the dissoc en assoc loop")
        (swap! remote-components (fn [m]
                                   (dissoc m
                                           (first (rdf-fn/subject-object-by-predicate m :dms-def/function)))))))
    (swap! remote-components conj rdf-content)))

(defn de-register
  [_ _ message]
  #_(println "@de-register - I'm new and all functionality is missing. It's a pitty\n" message)
  (let [msg-header (:datamos/logistic message)
        [[sender _]] (keep
                       (fn [s]
                         (if (= :dms-def/sender (rdf-fn/value-from-nested-map (conj {} s)))
                           s
                           nil))
                       (rdf-fn/predicate-filter msg-header #{:dms-def/transmit}))]
    (swap! the-registry dissoc sender)))

(def component-fns {:datamos/registration datamos.config/registration
                    :datamos/registry     datamos.config/register
                    :datamos/de-register  datamos.config/de-register})

(reset! config {:datamos-cfg/queue-name "config.datamos-fn"
                :dms-def/provides       component-fns})

(base/component-function {:datamos-cfg/component-type :datamos-fn/core
                          :datamos-cfg/component-fn   :datamos-fn/registry
                          :datamos-cfg/local-register datamos.config/local-register
                          :dms-def/provides component-fns})

(defn -main
  [& args]
  (dc/reset))

