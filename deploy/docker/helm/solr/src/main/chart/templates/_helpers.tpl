{{- define "edusharing_repository_search_solr.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_search_solr.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_search_solr.labels" -}}
{{ include "edusharing_repository_search_solr.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_repository_search_solr.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_repository_search_solr.labels.instance" -}}
{{ include "edusharing_repository_search_solr.labels.app" . }}
{{ include "edusharing_repository_search_solr.labels.version" . }}
{{- end -}}

{{- define "edusharing_repository_search_solr.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_repository_search_solr.labels.app" -}}
app: {{ include "edusharing_repository_search_solr.name" . }}
app.kubernetes.io/name: {{ include "edusharing_repository_search_solr.name" . }}
{{- end -}}

{{- define "edusharing_repository_search_solr.image" -}}
{{- $registry := default .Values.global.image.registry .Values.image.registry -}}
{{- $repository := default .Values.global.image.repository .Values.image.repository -}}
{{ $registry }}{{ if $registry }}/{{ end }}{{ $repository }}{{ if $repository }}/{{ end }}
{{- end -}}