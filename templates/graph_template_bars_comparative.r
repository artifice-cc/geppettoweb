m <- merge(control, comparison, by=c("simulation"))
result <- data.frame(value=c(), se=c(),
                     <(str (if (:fill graph) "fill=c()," ""))>
                     <(str (if (:color graph) "color=c()," ""))>
                     xfactor=c())

<(if (:fill graph) ">
for(fill in unique(m$<(:fill graph)>.y)) {
<")>

<(if (:color graph) ">
for(color in unique(m$<(:color graph)>.y)) {
<")>

<(if (:facethoriz graph) ">
for(facethoriz in unique(m$<(:facethoriz graph)>.y)) {
<")>

<(if (:facetvert graph) ">
for(facetvert in unique(m$<(:facetvert graph)>.y)) {
<")>

for(xfactor in unique(m$<(:xfactor graph)>.y)) {

  data <- subset(m,
                 <(if (:fill graph) "><(:fill graph)>.y == fill & <")>
                 <(if (:color graph) "><(:color graph)>.y == color & <")>
                 <(if (:facethoriz graph) "><(:facethoriz graph)>.y == facethoriz & <")>
                 <(if (:facetvert graph) "><(:facetvert graph)>.y == facetvert & <")>
                 <(:xfactor graph)>.y == xfactor)

  if(nrow(data) > 0) {
    d <- data.frame(label="Diff<(:yfield graph)>",
                    value=data$<(:yfield graph)>.y-data$<(:yfield graph)>.x)
    dse <- summarySE(d, measurevar="value", groupvars=c("label"))
    result <- rbind(result, data.frame(
                              <(str (if (:fill graph) "fill=fill," ""))>
                              <(str (if (:color graph) "color=color," ""))>
                              <(str (if (:facethoriz graph) "facethoriz=facethoriz," ""))>
                              <(str (if (:facetvert graph) "facetvert=facetvert," ""))>
                              xfactor=xfactor,
                              value=dse$value, se=dse$se))
  }
}
<(str (if (:fill graph) "}" ""))>
<(str (if (:color graph) "}" ""))>
<(str (if (:facethoriz graph) "}" ""))>
<(str (if (:facetvert graph) "}" ""))>

p <- ggplot(result, aes(x=factor(xfactor), y=value,
                        <(str (if (:fill graph) "fill=factor(fill)," ""))>
                        <(str (if (:color graph) "color=factor(color)," ""))>
                        ))
p <- p + geom_bar(position=position_dodge())
p <- p + geom_errorbar(aes(ymin=value-se, ymax=value+se),
                       width=.5, position=position_dodge(.9))
p <- p + scale_x_discrete("<(if (not= "" (:xlabel graph)) (:xlabel graph) (:xfactor graph))>")
p <- p + scale_y_continuous("<(if (not= "" (:ylabel graph)) (:ylabel graph) (:yfield graph))>")

<(if (or (:facethoriz graph) (:facetvert graph)) ">
p <- p + facet_grid(
<(str (if (:facetvert graph) "facetvert" "."))>
~
<(str (if (:facethoriz graph) "facethoriz" "."))>
)
<")>
