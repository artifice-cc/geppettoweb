result <- data.frame(value=c(), se=c(),
                     <(str (if (:fill graph) "fill=c()," ""))>
                     <(str (if (:color graph) "color=c()," ""))>
                     xfield=c())

<(if (:fill graph) ">
for(fill in unique(control$<(:fill graph)>)) {
<")>

<(if (:color graph) ">
for(color in unique(control$<(:color graph)>)) {
<")>

<(if (:facethoriz graph) ">
for(facethoriz in unique(control$<(:facethoriz graph)>)) {
<")>

<(if (:facetvert graph) ">
for(facetvert in unique(control$<(:facetvert graph)>)) {
<")>

for(xfield in unique(control$<(:xfield graph)>)) {

  data <- subset(control,
                 <(if (:fill graph) "><(:fill graph)> == fill & <")>
                 <(if (:color graph) "><(:color graph)> == color & <")>
                 <(if (:facethoriz graph) "><(:facethoriz graph)> == facethoriz & <")>
                 <(if (:facetvert graph) "><(:facetvert graph)> == facetvert & <")>
                 <(:xfield graph)> == xfield)

  if(nrow(data) > 0) {
    d <- data.frame(label="<(:yfield graph)>",
                    value=data$<(:yfield graph)>)
    dse <- summarySE(d, measurevar="value", groupvars=c("label"))
    result <- rbind(result, data.frame(
                              <(str (if (:fill graph) "fill=fill," ""))>
                              <(str (if (:color graph) "color=color," ""))>
                              <(str (if (:facethoriz graph) "facethoriz=facethoriz," ""))>
                              <(str (if (:facetvert graph) "facetvert=facetvert," ""))>
                              xfield=xfield,
                              value=dse$value, se=dse$se))
  }
}
<(str (if (:fill graph) "}" ""))>
<(str (if (:color graph) "}" ""))>
<(str (if (:facethoriz graph) "}" ""))>
<(str (if (:facetvert graph) "}" ""))>

p <- ggplot(result, aes(x=factor(xfield), y=value,
                        <(str (if (:fill graph) "fill=factor(fill)," ""))>
                        <(str (if (:color graph) "color=factor(color)," ""))>
                        ))
p <- p + geom_bar(position=position_dodge())
p <- p + geom_errorbar(aes(ymin=value-se, ymax=value+se),
                       width=.5, position=position_dodge(.9))
p <- p + scale_x_discrete("<(if (not= "" (:xlabel graph)) (:xlabel graph) (:xfield graph))>")
p <- p + scale_y_continuous("<(if (not= "" (:ylabel graph)) (:ylabel graph) (:yfield graph))>")

<(if (or (:facethoriz graph) (:facetvert graph)) ">
p <- p + facet_grid(
<(str (if (:facetvert graph) "facetvert" "."))>
~
<(str (if (:facethoriz graph) "facethoriz" "."))>
)
<")>
