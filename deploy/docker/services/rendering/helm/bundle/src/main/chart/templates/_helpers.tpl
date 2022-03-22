{{- define "edusharing_rendering.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_rendering.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_rendering.labels" -}}
{{ include "edusharing_rendering.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_rendering.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_rendering.labels.instance" -}}
{{ include "edusharing_rendering.labels.app" . }}
{{ include "edusharing_rendering.labels.version" . }}
{{- end -}}

{{- define "edusharing_rendering.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_rendering.labels.app" -}}
app: {{ include "edusharing_rendering.name" . }}
app.kubernetes.io/name: {{ include "edusharing_rendering.name" . }}
{{- end -}}
