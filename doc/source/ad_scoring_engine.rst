Scoring Engine
==============

This section covers the scoring engine configuration, installation, 
and running the scoring engine.


Configuration
-------------

The scoring engine is a tar ball, an artifact of the TAP Tool Kit build.
1) Download the artifact trustedanalytics-scoring.tar.gz to your TAP source area.
2) Unpack it; remove the tar ball::

    $ tar -xzf trustedanalytics-scoring.tar.gz
    $ rm trustedanalytics-scoring.tar.gz

3) Create a manifest file from the supplied template::

    $ cp manifest.yml.tpl manifest.yml
    
4) Edit two lines in the file.  Supply the names of your model (in place of SE) and your HDFS service (in place of hdfs-atk)::

    - name: SE # App name
    ...
    - hdfs-atk # hdfs service which holds the model tar file


Scoring Models Implementation
-----------------------------

The scoring engine is a generic engine and is oblivious to the streaming
scoring model.
The user provides the scoring implementation: specifically, the URI of the model.
This is the return value of the method <model>.publish()
Edit this location into the manifest.yml file, in the line TAR_ARCHIVE at the bottom of the file, such as::

    env:
      TAR_ARCHIVE: 'hdfs://nameservice1/user/tapdev/tap-dev01/models_f9ba22ece24e417fb72ec47eb5087a30.tar'


Starting the Scoring Engine Service
-----------------------------------

Once the manifest.yml file has been modified to point to the model URI,
the scoring engine can be launched and started with the following command,
run from the directory containing the manifest file::

    $ cf push

This will launch the rest server for the engine.
Allow 30-90 seconds for the engine to start and make itself known to the network.

The REST API is::

    GET /v1/models/[name]?data=[urlencoded record 1]


Scoring Client
--------------

Below is a sample python script to connect to the scoring engine::

    >>> import requests
    >>> import json
    >>> headers = {'Content-type': 'application/json',
    ...            'Accept': 'application/json,text/plain'}
    >>> r = requests.post('http://my-svm-model.10.10.47.181:9099/v1/score?data=2,17,-6', headers=headers)
    >>> r.text
    list(1)

**NOTE TO REVIEWERS:** *PET (our project) should maintain a set of test models on a public site.
This last example should change to connect to one of those models.*

