Rational of the leihs Admin-API
===============================

Context leihs Refactoring / Rewrite
-----------------------------------

* no "BigBang Rewrite" (contrast Madek)
* replace parts|routes one by one and deploy to production
* keep and do not touch leihs-legacy (as much as possible)

leihs Admin-API
---------------
* add Groups and, sync from evento|ZAPI for users and groups
* => adjust users, delegations, clean data, DB constraints, UI/UX
* API and Admin UI same entities/routes 
* combine both in one application, aka REST


API and UI 
----------
* use content-types 
* UI follows the necessities of the API
* the UI is the "API-Browser"


UI/UX no-frills Design
----------------------

goals: cost efficient implementation, maintainable (testing etc)
  
* eschew everything in the slightest way problematic 
  wrt testing or mobile platforms 

  * no autocomplete
  * no hover 
  * no dropdowns 

  * => feels very different, not necessarily worse or better 

* high rate of components reuse 

  * => not always optimal UX
  * => UI/UX easy to recognize and use

* shared code even between backend and frontend

  * => clojure + clojurescript

    
UI fast and efficient
---------------------

* single page app = SPA

* pages are fast and cheap, new page instead of dropdowns, modals, ...

* caveat: SPAs are very sensitive

  * frontend code may not break (!!!)
  * no fire and forget 
  * no "jquery style" programming
  * state management is very important 

  * => persistent data structures
  * => re-frame, see HackITZ 
  * => reagent = clojurescript + react
  


Sync
----

* import from ZAPI 
* mixed data, manual and managed: `org_id`
