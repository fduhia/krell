(ns cljs.repl.rn.mdns
  (:require [clojure.string :as string])
  (:import [java.net Inet4Address InetAddress NetworkInterface Socket URI]
           [javax.jmdns JmDNS ServiceListener]))

(defn rn-repl? [name]
  (string/starts-with? name "rn.repl"))

(defn setup
  "Sets up mDNS to populate atom supplied in name-endpoint-map with discoveries.
  Returns a function that will tear down mDNS."
  [{:keys [reg-type endpoint-map match-name]}]
  {:pre [(string? reg-type)]
   :post [(fn? %)]}
  (let [mdns-service (JmDNS/create)
        service-listener
        (reify ServiceListener
          (serviceAdded [_ service-event]
            (let [type (.getType service-event)
                  name (.getName service-event)]
              (when (and (= reg-type type) (match-name name))
                (.requestServiceInfo mdns-service type name 1))))
          (serviceRemoved [_ service-event]
            (swap! endpoint-map dissoc (.getName service-event)))
          (serviceResolved [_ service-event]
            (let [type (.getType service-event)
                  name (.getName service-event)]
              (when (and (= reg-type type) (match-name name))
                (let [entry {name (let [info (.getInfo service-event)]
                                    {:address (.getHostAddress (.getAddress info))
                                     :port    (.getPort info)})}]
                  (swap! endpoint-map merge entry))))))]
    (.addServiceListener mdns-service reg-type service-listener)
    (fn []
      (.removeServiceListener mdns-service reg-type service-listener)
      (.close mdns-service))))
