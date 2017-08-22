(ns datamos.config.core
  (:refer-clojure)
  (:require [mount.core :as mnt :refer [defstate]]
            [datamos
             [core :as dc]
             [communication :as dcom]
             [base :as base]
             [rdf-function :as rdf-fn]
             [messaging :as dm]
             [sign-up :as sup]
             [module-helpers :as hlp]]
            [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]))

(defonce ^:private config (atom {}))

(defonce ^:private the-registry (atom {}))

(defonce remote-components (atom {}))

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

(defn init-registry
  []
  (do
    (reset! the-registry {})
    (reset! remote-components {})))

(defstate initialize
          :start (init-registry))

(defn registration
  [_ _ message]
  (let [msg-header (:datamos/logistic message)
        sender-uri (apply first (filter (fn [x] (= :dms-def/sender (rdf-fn/value-from-nested-map (conj {} x)))) (rdf-fn/predicate-filter msg-header #{:dms-def/transmit})))]
    (log/debug "@registration" (log/get-env))
    (swap!
      the-registry
      conj
      (rdf-fn/predicate-filter
        (rdf-fn/message-content message)
        registry-predicates-set))
    (dcom/speak dcom/speak-connection dm/exchange base/component sender-uri :dmsfn-def/module-id :datamos-fn/registry @the-registry)))

(defn local-register
  []
  @remote-components)

(defn de-register
  [_ _ message]
  (let [msg-header (:datamos/logistic message)
        [[sender _]] (keep
                       (fn [s]
                         (if (= :dms-def/sender (rdf-fn/value-from-nested-map (conj {} s)))
                           s
                           nil))
                       (rdf-fn/predicate-filter msg-header #{:dms-def/transmit}))]
    (log/debug "\n  @de-register" "\n    msg-header" msg-header "\n    sender" sender)
    (swap! the-registry dissoc sender)))

(def component-fns (merge
                     {:datamos-fn/registration datamos.config.core/registration
                      :datamos-fn/de-register  datamos.config.core/de-register}
                     (hlp/local-module-register remote-components)))

(reset! config {:datamos/queue-name "config.datamos-fn"
                :dms-def/provides       component-fns})

(base/component-function {:dmsfn-def/module-type :dmsfn-def/core
                          :dmsfn-def/module-name   :dmsfn-def/config
                          :dmsfn-def/local-register (datamos.config.core/local-register)
                          :dms-def/provides           datamos.config.core/component-fns})

(defn -main
  [& args]
  (do
    (log/info "@-main - Config module starting")
    (set-refresh-dirs "src/datamos/config")
    (dc/reset)))