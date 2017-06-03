(ns swarmpit.component.repository.v2-list
  (:require [material.component :as comp]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [clojure.walk :as walk]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(def cursor [:page :repository :list :v2 :data])

(def headers ["Name"])

(def render-item-keys
  [[:name]])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- repository-handler
  [name query]
  (ajax/GET "v2/repositories"
            {:headers {"Authorization" (storage/get "token")}
             :params  {:registryName    name
                       :repositoryQuery query}
             :handler (fn [response]
                        (let [res (walk/keywordize-keys response)]
                          (state/set-value res cursor)))}))

(rum/defc repository-list < rum/reactive [registry-name]
  (let [items (state/react cursor)
        repository (fn [index] (:name (nth items index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Search in registry"
          :onChange (fn [_ v]
                      (repository-handler registry-name v))})]]
     (comp/mui
       (comp/table
         {:key         "tbl"
          :selectable  false
          :onCellClick (fn [i]
                         (dispatch! (str "/#/services/create/wizard/config?"
                                         "repository=" (repository i)
                                         "&registry=" registry-name)))}
         (comp/list-table-header headers)
         (comp/list-table-body items
                               render-item
                               render-item-keys)))]))

(defn mount!
  [registry-name]
  (rum/mount (repository-list registry-name) (.getElementById js/document "content")))