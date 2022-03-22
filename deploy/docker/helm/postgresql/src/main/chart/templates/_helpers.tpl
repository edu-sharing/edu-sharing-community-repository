{{- define "edusharing_postgresql.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_postgresql.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_postgresql.labels" -}}
{{ include "edusharing_postgresql.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_postgresql.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_postgresql.labels.instance" -}}
{{ include "edusharing_postgresql.labels.app" . }}
{{ include "edusharing_postgresql.labels.version" . }}
{{- end -}}

{{- define "edusharing_postgresql.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_postgresql.labels.app" -}}
app: {{ include "edusharing_postgresql.name" . }}
app.kubernetes.io/name: {{ include "edusharing_postgresql.name" . }}
{{- end -}}
