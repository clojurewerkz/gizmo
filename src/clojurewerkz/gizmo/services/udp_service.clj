(ns clojurewerkz.gizmo.services.udp-service
  ^{:doc "Example service implementation, udp listening service" }
  (:require [clojure.java.io :as io])
  (:use [clojurewerkz.gizmo.service])
  (:import [java.net InetSocketAddress DatagramSocket DatagramPacket]))

(alter-var-root #'*out* (constantly *out*))

(defn- bind [host port]
  (DatagramSocket. (InetSocketAddress. host port)))

(def counter (atom 0))

(defn run [&{:keys [port host service]}]
  (println "Starting udp service with host '" host "' and port " port)
  (loop [socket (bind host port)]
    (reset-state service {:status :running :socket socket})
    (let [received-data (byte-array 10000)
          packet (DatagramPacket. received-data (alength received-data))]
      (.receive socket packet)
      (try
        (let [msg (String. (.getData packet) "UTF-8")]
          (swap! counter inc))
        (catch Exception e
          (.close socket)
          (println "Handling an error while listening to UDP socket: %s. Reconnecting..." e)))
      (println 'alive (alive? service) )
      (when (alive? service)
        (recur (if (.isClosed socket)
                 (bind host port)
                 socket))))))

(defn shutdown [{:keys [socket]}]
  (.close socket))

(defservice udp-service
  :config {:host "127.0.0.1" :port 3344}
  :alive (fn [service]
           (let [{:keys [status socket]} (state service)]
             (and socket status
                  (= :running status)
                  (thread service)
                  (.isAlive (thread service)))))
  :stop (fn [service]
          (when-let [socket (:socket (state service))]
            (reset-state service {:status :stopped})
            (.close socket)))
  :start (fn [service]
           (let [{:keys [host port]} (config service)]
             (run :service service :host host :port port))))

(defn publish
  "Test function for publishing to service"
  [host port msg]
  (let [buf (.getBytes msg)
        socket (DatagramSocket.)]
    (.send
     socket
     (java.net.DatagramPacket. buf
                               (count buf)
                               (java.net.InetAddress/getByName host)
                               port))
    (.close socket)))

;; (publish "localhost" 3344 "asd")
