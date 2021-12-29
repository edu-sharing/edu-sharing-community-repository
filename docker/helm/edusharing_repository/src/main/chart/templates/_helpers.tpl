{{- define "edusharing_repository.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository.labels" -}}
{{ include "edusharing_repository.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_repository.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_repository.labels.instance" -}}
{{ include "edusharing_repository.labels.app" . }}
{{ include "edusharing_repository.labels.version" . }}
{{- end -}}

{{- define "edusharing_repository.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_repository.labels.app" -}}
app: {{ include "edusharing_repository.name" . }}
app.kubernetes.io/name: {{ include "edusharing_repository.name" . }}
{{- end -}}
