{{- define "edusharing_elasticsearch.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_elasticsearch.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_elasticsearch.labels" -}}
{{ include "edusharing_elasticsearch.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_elasticsearch.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_elasticsearch.labels.instance" -}}
{{ include "edusharing_elasticsearch.labels.app" . }}
{{ include "edusharing_elasticsearch.labels.version" . }}
{{- end -}}

{{- define "edusharing_elasticsearch.labels.app" -}}
app: {{ include "edusharing_elasticsearch.name" . }}
app.kubernetes.io/name: {{ include "edusharing_elasticsearch.name" . }}
{{- end -}}

{{- define "edusharing_elasticsearch.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}
