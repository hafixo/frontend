(ns frontend.utils.state
  (:require [frontend.state :as state]
            [frontend.utils.vcs-url :as vcs-url]
            [frontend.utils.seq :refer [find-index]]))

(defn set-dashboard-crumbs [state {:keys [org repo branch]}]
  (assoc-in state state/crumbs-path (vec (concat
                                          (when org [{:type :org
                                                      :username org}])
                                          (when repo [{:type :project
                                                       :username org :project repo}])
                                          (when branch [{:type :project-branch
                                                         :username org :project repo :branch branch}])))))

(defn reset-current-build [state]
  (assoc state :current-build-data {:build nil
                                    :usage-queue-data {:builds nil
                                                       :show-usage-queue false}
                                    :artifact-data {:artifacts nil
                                                    :show-artifacts false}
                                    :current-container-id 0
                                    :container-data {:current-container-id 0
                                                     :containers nil}}))

(defn reset-current-project [state]
  (assoc state :current-project-data {:project nil
                                      :plan nil
                                      :settings {}
                                      :tokens nil
                                      :envvars nil}))

(defn stale-current-project? [state project-name]
  (and (get-in state state/project-path)
       ;; XXX: check for url-escaped characters (e.g. /)
       (not= project-name (vcs-url/project-name (get-in state (conj state/project-path :vcs_url))))))

(defn find-repo-index
  "Path for a given repo. Login is the username, type is user or org, name is the repo name."
   [state login type repo-name]
   (when-let [repos (get-in state (state/repos-path login type))]
     (find-index #(= repo-name (:name %)) repos)))
