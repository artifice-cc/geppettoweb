function(doc) {
    if(doc["type"] == "run") {
        emit([doc["paramsid"], doc["paramsrev"]],
             {"_id": doc["_id"],
              "time": doc["time"],
              "problem": doc["problem"],
              "count": doc["comparative"].length,
              "paramsid": doc["paramsid"],
              "paramsname": doc["paramsname"],
              "paramsrev": doc["paramsrev"],
              "commit": doc["commit"],
              "branch": doc["branch"]});
    }
}