m <- merge(control, comparison, by=c("simulation"))
result <- data.frame(value=c(), se=c(),
                     <(str (if (:fill graph) "fill=c()," ""))>
                     xfactor=c())
<(if (:fill graph) ">
for(fill in unique(m$<(:fill graph)>.y)) {
<")>
for(xfactor in unique(m$<(:xfactor graph)>.y)) {
  data <- subset(m,
                 <(if (:fill graph) "><(:fill graph)>.y == fill & <")>
                 <(:xfactor graph)>.y == xfactor)

  if(nrow(data) > 0) {
    d <- data.frame(label="Diff<(:yfield graph)>",
                    value=data$<(:yfield graph)>.y-data$<(:yfield graph)>.x)
    dse <- summarySE(d, measurevar="value", groupvars=c("label"))
    result <- rbind(result, data.frame(
                              <(str (if (:fill graph) "fill=fill," ""))>
                              xfactor=xfactor,
                              value=dse$value, se=dse$se))
  }
}
<(str (if (:fill graph) "}" ""))>

p <- ggplot(result, aes(x=factor(xfactor), y=value,
                        <(str (if (:fill graph) "fill=factor(fill)," ""))>))
p <- p + geom_bar(position=position_dodge())
p <- p + geom_errorbar(aes(ymin=value-se, ymax=value+se),
                       width=.5, position=position_dodge(.9))
p <- p + scale_fill_grey()
p <- p + scale_x_discrete("<(:xlabel graph)>")
p <- p + scale_y_continuous("<(:ylabel graph)>")
