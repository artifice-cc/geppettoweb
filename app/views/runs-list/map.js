function(doc) {
    if(doc["type"] == "run") {
        emit(doc["time"],
             {"_id": doc["_id"],
              "problem": doc["problem"],
              "time": doc["time"],
              "count": doc["comparative"].length,
              "paramsid": doc["paramsid"],
              "paramsname": doc["paramsname"],
              "paramsrev": doc["paramsrev"],
              "commit": doc["commit"],
              "branch": doc["branch"]});
    }
}