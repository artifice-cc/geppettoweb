function(doc) {
    if(doc["type"] == "comparative") {
        for(var f in doc) {
            if(f != "Problem" && f != "Seed" && f != "type" &&
               f != "runid" && f != "_rev" && f != "_id") {
                emit([doc["Problem"], f], null);
            }
        }
    }
}