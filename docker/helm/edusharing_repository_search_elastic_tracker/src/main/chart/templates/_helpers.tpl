{{- define "edusharing_repository_search_elastic_tracker.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_search_elastic_tracker.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_search_elastic_tracker.labels" -}}
{{ include "edusharing_repository_search_elastic_tracker.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_repository_search_elastic_tracker.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_repository_search_elastic_tracker.labels.instance" -}}
{{ include "edusharing_repository_search_elastic_tracker.labels.app" . }}
{{ include "edusharing_repository_search_elastic_tracker.labels.version" . }}
{{- end -}}

{{- define "edusharing_repository_search_elastic_tracker.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_repository_search_elastic_tracker.labels.app" -}}
app: {{ include "edusharing_repository_search_elastic_tracker.name" . }}
app.kubernetes.io/name: {{ include "edusharing_repository_search_elastic_tracker.name" . }}
{{- end -}}
