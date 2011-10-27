function(doc) {
    if(doc["type"] == "run") {
        emit(doc["time"],
             {"_id": doc["_id"],
              "problem": doc["problem"],
              "time": doc["time"],
              "username": doc["username"],
              "hostname": doc["hostname"],
              "count": doc["control"].length,
              "paramsid": doc["paramsid"],
              "paramsname": doc["paramsname"],
              "paramsrev": doc["paramsrev"],
              "paramstype": doc["paramstype"],
              "commit": doc["commit"],
              "branch": doc["branch"]});
    }
}