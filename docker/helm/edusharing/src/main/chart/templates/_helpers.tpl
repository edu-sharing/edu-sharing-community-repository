{{- define "edusharing.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing.labels" -}}
{{ include "edusharing.labels.instance" . }}
helm.sh/chart: {{ include "edusharing.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing.labels.instance" -}}
{{ include "edusharing.labels.app" . }}
{{ include "edusharing.labels.version" . }}
{{- end -}}

{{- define "edusharing.labels.app" -}}
app: {{ include "edusharing.name" . }}
app.kubernetes.io/name: {{ include "edusharing.name" . }}
{{- end -}}

{{- define "edusharing.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing.dockerconfigjson" }}
{{- printf "{\"auths\":{\"%s\":{\"username\":\"%s\",\"password\":\"%s\",\"auth\":\"%s\"}}}" .server .username .password (printf "%s:%s" .username .password | b64enc) | b64enc }}
{{- end }}