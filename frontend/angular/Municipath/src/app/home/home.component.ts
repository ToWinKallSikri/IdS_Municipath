import { Component } from '@angular/core';
import { ComuneService } from '../services/comune.service';
import { City } from '../models/City';
import { ActivatedRoute, Router } from '@angular/router';
import { AfterViewInit, ViewEncapsulation   } from '@angular/core';
import * as L from 'leaflet';
import { CheckService } from '../services/check.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class HomeComponent implements AfterViewInit  {
  public comuni: City[] = [];

  constructor(private route : ActivatedRoute, private comuneService : ComuneService,
     private router : Router, private checkService : CheckService) {}

  private map: any;
  private markers : any[] = []
  private marker: any;
  private city = 'https://i.postimg.cc/GpP8xRfs/Authority.png';
  private empty = 'https://i.postimg.cc/ZngYcZfq/immagine-2024-01-24-113850127-png.png';
  private isManager : boolean = false;
  search = '';

  async ngAfterViewInit(): Promise<void> {
    this.isManager = (await firstValueFrom(this.checkService.checkManager())).response == 'true';
    this.map = L.map('map').setView([44, 13], 5.5);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(this.map);
        this.map.setMaxZoom(12);
        this.map.setMinZoom(5.5);
        this.map.setMaxBounds([
          [47.0, 6.0],
          [35.0, 19.0]
        ]);
    this.route.queryParams.subscribe(params => {
      this.search = params['id'] ? params['id'] : '';
      this.comuneService.getCities(this.search).subscribe((comuniBE) => {
        this.comuni = comuniBE.map(c => this.comuneService.makeCity(c));
        this.comuni.forEach(c =>  this.addMarker(c));
        this.map.on('click', (event: any) => {
          this.addEmptyMarker(event.latlng.lat, event.latlng.lng);
        });
      });
    });
  }

  private addEmptyMarker(lat : number, lng: number){
    if(this.marker)
      this.map.removeLayer(this.marker);
    var myIcon = L.icon({
      iconUrl: this.empty,
      iconSize: [28, 40],
      popupAnchor: [0, -26]
  });
    this.marker = L.marker([lat, lng], {icon:myIcon}).addTo(this.map);
    if(this.isManager){
      this.marker.bindPopup(`<button onClick="location.href='/makecity/${lat}/${lng}'">Crea Comune</button>`,  {closeButton: false})
      .on('click', (event: any) => {
        this.marker.openPopup();
      });
    }
  }

  onChange(valore : string) {
    this.markers.forEach(m => this.map.removeLayer(m));
    this.comuneService.getCities(this.search).subscribe((comuniBE) => {
      this.comuni = comuniBE;
      this.comuni.forEach(c =>  this.addMarker(c));
      this.router.navigateByUrl('/?id='+valore);
    });
  }

  private addMarker(city: City): void {
    var myIcon = L.icon({
      iconUrl: this.city,
      iconSize: [28, 40],
      popupAnchor: [0, -26]
  });
    const marker = L.marker([city.pos.lat, city.pos.lng], {icon:myIcon}).addTo(this.map);
    if(this.isManager){
    marker.bindPopup(`<style>.pubtn{margin: auto; display: block;}</style><p style="text-align: center"><b>${city.name}<br>${city.cap}</b></p><button class="pubtn" onClick="location.href='/city/${city.id}'">Visita Comune</button><button class="pubtn" onClick="location.href='/updatecity/${city.id}'">Modifica Comune</button><button class="pubtn" onClick="location.href='/deletecity/${city.id}'">Elimina Comune</button>`,  {closeButton: false})
    } else {
      marker.bindPopup(`<p style="text-align: center"><b>${city.name}<br>${city.cap}</b></p><br><button onClick="location.href='/city/${city.id}'">Visita Comune</button>`,  {closeButton: false})
    }
    marker.on('click', (event: any) => {
      marker.openPopup();
    });
    this.markers.push(marker);
  }

}
