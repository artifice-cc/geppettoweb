function(doc) {
    if(doc["type"] == "run") {
        emit([doc["paramsid"], doc["paramsrev"]],
             {"_id": doc["_id"],
              "time": doc["time"],
              "graph-count": (doc["graphs"] ? doc["graphs"].length : 0),
              "analysis-count": (doc["analysis"] ? doc["analysis"].length : 0),
              "username": doc["username"],
              "hostname": doc["hostname"],
              "problem": doc["problem"],
              "count": doc["comparative"].length,
              "paramsid": doc["paramsid"],
              "paramsname": doc["paramsname"],
              "paramsrev": doc["paramsrev"],
              "commit": doc["commit"],
              "branch": doc["branch"]});
    }
}