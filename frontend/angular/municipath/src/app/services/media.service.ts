import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class MediaService {

  private pathUrl = '/api/v1/path';
  private fileUrl = '/api/v1/media';

  constructor(private http: HttpClient) { }

  
  getPath(files: File[]): Observable<string[]> {
    const formData: FormData = new FormData();
    Array.from(files).forEach((file) => {
      formData.append('files', file, file.name);
    });
    return this.http.post<string[]>(environment.baseUrl + this.pathUrl, formData);
  }

  getFile(path: string): Observable<any> {
    let header = new HttpHeaders().append("path", path);
    return this.http.get(environment.baseUrl + this.fileUrl, { headers: header, responseType: 'blob'});
  }

}