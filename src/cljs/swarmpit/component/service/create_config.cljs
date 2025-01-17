(ns swarmpit.component.service.create-config
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-configs :as configs]
            [swarmpit.component.service.form-hosts :as hosts]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-logdriver :as logdriver]
            [swarmpit.component.service.form-resources :as resources]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def doc-services-link "https://docs.docker.com/engine/swarm/services/")

(defn- create-service-handler
  []
  (let [settings (state/get-value settings/form-value-cursor)
        ports (state/get-value ports/form-value-cursor)
        networks (state/get-value networks/form-value-cursor)
        secrets (state/get-value secrets/form-value-cursor)
        configs (state/get-value configs/form-value-cursor)
        hosts (state/get-value hosts/form-value-cursor)
        variables (state/get-value variables/form-value-cursor)
        labels (state/get-value labels/form-value-cursor)
        logdriver (state/get-value logdriver/form-value-cursor)
        resources (state/get-value resources/form-value-cursor)
        deployment (state/get-value deployment/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :service-create)
      {:params     (-> settings
                       (assoc :ports ports)
                       (assoc :networks networks)
                       (assoc :mounts (mounts/normalize))
                       (assoc :secrets (when-not (empty? (state/get-value (conj secrets/form-state-cursor :list))) secrets))
                       (assoc :configs (when-not (empty? (state/get-value (conj configs/form-state-cursor :list))) configs))
                       (assoc :hosts hosts)
                       (assoc :variables variables)
                       (assoc :labels labels)
                       (assoc :logdriver logdriver)
                       (assoc :resources resources)
                       (assoc :deployment deployment))
       :state      [:processing?]
       :on-success (fn [{:keys [response origin?]}]
                     (when origin?
                       (dispatch!
                         (routes/path-for-frontend :service-info (select-keys response [:id]))))
                     (message/info
                       (str "Service " (:id response) " has been created.")))
       :on-error   (fn [{:keys [response]}]
                     (message/error
                       (str "Service creation failed. " (:error response))))})))

(defn- init-form-state
  []
  (state/set-value {:processing? false} state/form-state-cursor)
  (state/set-value {:valid? false
                    :tags   []} settings/form-state-cursor)
  (state/set-value {:volumes []} mounts/form-state-cursor)
  (state/set-value {:list []} secrets/form-state-cursor)
  (state/set-value {:list []} configs/form-state-cursor)
  (state/set-value {:list []} networks/form-state-cursor)
  (state/set-value {:list []} placement/form-state-cursor)
  (state/set-value {:names []} labels/form-state-cursor)
  (state/set-value {:valid? true} resources/form-state-cursor))

(defn- init-form-value
  [repository]
  (state/set-value {:repository  {:name repository
                                  :tag  ""}
                    :serviceName ""
                    :mode        "replicated"
                    :replicas    1} settings/form-value-cursor)
  (state/set-value [] ports/form-value-cursor)
  (state/set-value [] networks/form-value-cursor)
  (state/set-value [] mounts/form-value-cursor)
  (state/set-value [] secrets/form-value-cursor)
  (state/set-value [] configs/form-value-cursor)
  (state/set-value [] hosts/form-value-cursor)
  (state/set-value [] variables/form-value-cursor)
  (state/set-value [] labels/form-value-cursor)
  (state/set-value {:name "json-file"
                    :opts []} logdriver/form-value-cursor)
  (state/set-value {:autoredeploy  false
                    :restartPolicy {:condition "any"
                                    :delay     5
                                    :attempts  0}
                    :update        {:parallelism   1
                                    :delay         0
                                    :order         "stop-first"
                                    :failureAction "pause"}
                    :rollback      {:parallelism   1
                                    :delay         0
                                    :order         "stop-first"
                                    :failureAction "pause"}} deployment/form-value-cursor)
  (state/set-value {:reservation {:cpu    0.000
                                  :memory 0}
                    :limit       {:cpu    0.000
                                  :memory 0}} resources/form-value-cursor)
  (state/set-value [] placement/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [repository]} :params}]
      (init-form-state)
      (init-form-value repository)
      (mounts/volumes-handler)
      (networks/networks-handler)
      (logdriver/drivers-handler)
      (secrets/secrets-handler)
      (when (<= 1.30 (state/get-value [:docker :api]))
        (configs/configs-handler))
      (placement/placement-handler)
      (labels/labels-handler)
      (settings/tags-handler repository))))

(rum/defc form-settings < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section "General")
    (settings/form false)))

(rum/defc form-ports < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section
      "Ports"
      (comp/button
        {:color   "primary"
         :onClick ports/add-item}
        (comp/svg icon/add-small-path) "Add port"))
    (ports/form)))

(rum/defc form-mounts < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section
      "Mounts"
      (comp/button
        {:color   "primary"
         :onClick mounts/add-item}
        (comp/svg icon/add-small-path) "Add mount"))
    (mounts/form)))

(rum/defc form-secrets < rum/reactive []
  (comp/grid
    {:item true
     :xs   12}
    (form/section
      "Secrets"
      (comp/button
        {:color   "primary"
         :onClick secrets/add-item}
        (comp/svg icon/add-small-path) "Add secret"))
    (secrets/form)))

(rum/defc form-configs < rum/reactive []
  (comp/grid
    {:item true
     :xs   12}
    (form/section
      "Configs"
      (comp/button
        {:color   "primary"
         :onClick configs/add-item}
        (comp/svg icon/add-small-path) "Add config"))
    (configs/form)))

(rum/defc form-hosts < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section
      "Extra hosts"
      (comp/button
        {:color   "primary"
         :onClick hosts/add-item}
        (comp/svg icon/add-small-path) "Add host mapping"))
    (hosts/form)))

(rum/defc form-variables < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section
      "Environment variables"
      (comp/button
        {:color   "primary"
         :onClick variables/add-item}
        (comp/svg icon/add-small-path) "Add variable"))
    (variables/form)))

(rum/defc form-labels < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section
      "Labels"
      (comp/button
        {:color   "primary"
         :onClick labels/add-item}
        (comp/svg icon/add-small-path) "Add label"))
    (labels/form)))

(rum/defc form-logdriver < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section "Log driver")
    (logdriver/form)))

(rum/defc form-resources < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section "Resources")
    (resources/form)))

(rum/defc form-deployment < rum/static []
  (comp/grid
    {:item true
     :xs   12}
    (form/section
      "Deployment")
    (deployment/form)))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [settings-state (state/react settings/form-state-cursor)
        resources-state (state/react resources/form-state-cursor)
        {:keys [processing?]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          [:div.Swarmpit-form-paper
           (common/edit-title "Create a new service"  (str "from " (get-in (state/get-value settings/form-value-cursor) [:repository :name])))
           (comp/grid
             {:container true
              :className "Swarmpit-form-main-grid"
              :spacing   40}
             (comp/grid
               {:item true
                :xs   12
                :sm   12
                :md   12
                :lg   8
                :xl   8}
               (comp/grid
                 {:container true
                  :spacing   40}
                 (form-settings)
                 (form-ports)
                 (form-mounts)
                 (form-secrets)
                 (when (<= 1.30 (state/get-value [:docker :api]))
                   (form-configs))
                 (form-hosts)
                 (form-variables)
                 (form-labels)
                 (form-logdriver)
                 (form-resources)
                 (form-deployment)
                 (comp/grid
                   {:item true
                    :xs   12}
                   (html
                     [:div.Swarmpit-form-buttons
                      (composite/progress-button
                        "Create"
                        create-service-handler
                        processing?)]))))
             (comp/grid
               {:item true
                :xs   12
                :sm   12
                :md   12
                :lg   4
                :xl   4}
               (form/open-in-new "Learn more about compose" doc-services-link)))]]]))))