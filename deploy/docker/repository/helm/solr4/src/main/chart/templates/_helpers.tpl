{{- define "edusharing_repository_search_solr4.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_search_solr4.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_search_solr4.labels" -}}
{{ include "edusharing_repository_search_solr4.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_repository_search_solr4.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_repository_search_solr4.labels.instance" -}}
{{ include "edusharing_repository_search_solr4.labels.app" . }}
{{ include "edusharing_repository_search_solr4.labels.version" . }}
{{- end -}}

{{- define "edusharing_repository_search_solr4.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_repository_search_solr4.labels.app" -}}
app: {{ include "edusharing_repository_search_solr4.name" . }}
app.kubernetes.io/name: {{ include "edusharing_repository_search_solr4.name" . }}
{{- end -}}

{{- define "edusharing_repository_search_solr4.image" -}}
{{- $registry := default .Values.global.image.registry .Values.image.registry -}}
{{- $repository := default .Values.global.image.repository .Values.image.repository -}}
{{ $registry }}{{ if $registry }}/{{ end }}{{ $repository }}{{ if $repository }}/{{ end }}
{{- end -}}