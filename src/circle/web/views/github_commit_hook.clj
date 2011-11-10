(ns circle.web.views.github-commit-hook
  (:require [org.danlarkin.json :as json])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        [noir.request :only (*request*)])
  (:require [circle.backend.build :as build])
  (:require [circle.backend.build.run :as run])
  (:require [circle.backend.project.circle :as circle])
  (:use [circle.backend.action.vcs :only (github-http->ssh)])
  (:use [circle.backend.build :only (extend-group-with-revision)])
  (:use [clojure.tools.logging :only (infof)])
  (:use circle.web.views.common))

(def sample-json (json/decode "{
  \"before\": \"5aef35982fb2d34e9d9d4502f6ede1072793222d\",
  \"repository\": {
    \"url\": \"http://github.com/defunkt/github\",
    \"name\": \"github\",
    \"description\": \"You're lookin' at it.\",
    \"watchers\": 5,
    \"forks\": 2,
    \"private\": 1,
    \"owner\": {
      \"email\": \"chris@ozmm.org\",
      \"name\": \"defunkt\"
    }
  },
  \"commits\": [
    {
      \"id\": \"41a212ee83ca127e3c8cf465891ab7216a705f59\",
      \"url\": \"http://github.com/defunkt/github/commit/41a212ee83ca127e3c8cf465891ab7216a705f59\",
      \"author\": {
        \"email\": \"chris@ozmm.org\",
        \"name\": \"Chris Wanstrath\"
      },
      \"message\": \"okay i give in\",
      \"timestamp\": \"2008-02-15T14:57:17-08:00\",
      \"added\": [\"filepath.rb\"]
    },
    {
      \"id\": \"de8251ff97ee194a289832576287d6f8ad74e3d0\",
      \"url\": \"http://github.com/defunkt/github/commit/de8251ff97ee194a289832576287d6f8ad74e3d0\",
      \"author\": {
        \"email\": \"chris@ozmm.org\",
        \"name\": \"Chris Wanstrath\"
      },
      \"message\": \"update pricing a tad\",
      \"timestamp\": \"2008-02-15T14:36:34-08:00\"
    }
  ],
  \"after\": \"de8251ff97ee194a289832576287d6f8ad74e3d0\",
  \"ref\": \"refs/heads/master\"
}"))

;;; SECURITY TODO: this data is unverified - consider DDOS possibility
;;; at least.
(defn process-json [github-json]
  (def last-json github-json)
  (when (= "CircleCI" (-> github-json :repository :name))
    (let [build (circle/circle-build)]
      (dosync
       (alter build merge 
              {:notify-email (-> github-json :repository :owner :email)
               :vcs-url (github-http->ssh (-> github-json :repository :url))
               :repository (-> github-json :repository)
               :commits (-> github-json :commits)
               :vcs-revision (-> github-json :commits last :id)
               :num-nodes 1}))
      (infof "process-json: build: %s" @build)
      (run/run-build build))))

(defpage [:post "/github-commit"] []
  (infof "github post: %s" *request*)
  (def last-request *request*)
  (let [github-json (json/decode (-> *request* :params :payload))]
    (def last-future (future (process-json github-json)))
    {:status 200 :body ""}))