m <- merge(control, comparison, by=c("simulation"))
result <- data.frame(xfactor=c(), value=c(), se=c())
for(xfactor in unique(m$<(:xfactor graph)>.y)) {
  data <- subset(m, <(:xfactor graph)>.y == xfactor)

  if(nrow(data) > 0) {
    d <- data.frame(label="Diff<(:yfield graph)>",
                    value=data$<(:yfield graph)>.y-data$<(:yfield graph)>.x)
    dse <- summarySE(d, measurevar="value", groupvars=c("label"))
    result <- rbind(result, data.frame(xfactor=xfactor, value=dse$value, se=dse$se))
  }
}

p <- ggplot(result, aes(x=factor(xfactor), y=value))
p <- p + geom_bar(position=position_dodge())
p <- p + geom_errorbar(aes(ymin=value-se, ymax=value+se),
                       width=.5, position=position_dodge(.9))
p <- p + scale_fill_grey()
p <- p + scale_x_discrete("<(:xlabel graph)>")
p <- p + scale_y_continuous("<(:ylabel graph)>")
